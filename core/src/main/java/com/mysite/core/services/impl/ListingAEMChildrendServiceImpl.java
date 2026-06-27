package com.mysite.core.services.impl;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.mysite.core.services.ListingAEMChildrendService;

import lombok.extern.slf4j.Slf4j;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Triển khai 3 cách liệt kê descendant page để so sánh.
 *
 * <p><b>Tiêu chí thống nhất cho cả 3 cách:</b> liệt kê tất cả node {@code cq:Page}
 * là con/cháu của {@code root}, <b>KHÔNG bao gồm chính node root</b>. Nhờ vậy cả 3
 * cho ra cùng một tập kết quả, chỉ khác nhau về hiệu năng.
 *
 * @see ListingAEMChildrendService
 */
@Slf4j
@Component(service = ListingAEMChildrendService.class)
public class ListingAEMChildrendServiceImpl implements ListingAEMChildrendService {

    @Reference
    private QueryBuilder queryBuilder;

    // ---------------------------------------------------------------------
    // Cách 1: JCR Query (QueryBuilder) — tốn tài nguyên nhất
    // ---------------------------------------------------------------------
    @Override
    public List<String> listUsingJcrQuery(Resource root) {
        List<String> result = new ArrayList<>();
        if (root == null) {
            return result;
        }

        ResourceResolver resolver = root.getResourceResolver();
        Session session = resolver.adaptTo(Session.class);
        if (session == null) {
            log.warn("Không adapt được Session từ resolver cho path: {}", root.getPath());
            return result;
        }

        String rootPath = root.getPath();
        Map<String, String> predicate = new HashMap<>();
        predicate.put("path", rootPath);
        predicate.put("type", NameConstants.NT_PAGE);
        predicate.put("p.limit", "-1");

        Query query = queryBuilder.createQuery(PredicateGroup.create(predicate), session);
        SearchResult searchResult = query.getResult();

        // QUAN TRỌNG: KHÔNG đóng resolver lấy từ hit/root — nó thuộc về request hiện tại.
        // Đóng nó sẽ làm hỏng phần render còn lại (đây chính là "leaking reference" anti-pattern).
        for (Hit hit : searchResult.getHits()) {
            try {
                String path = hit.getPath();
                // Loại bỏ chính node root để thống nhất tiêu chí với 2 cách còn lại.
                if (!rootPath.equals(path)) {
                    result.add(path);
                }
            } catch (RepositoryException e) {
                log.error("Lỗi đọc path từ search hit", e);
            }
        }
        return result;
    }

    // ---------------------------------------------------------------------
    // Cách 2: Page.listChildren + PageFilter (deep) — đơn giản nhưng chậm hơn
    // ---------------------------------------------------------------------
    @Override
    public List<String> listUsingPageManager(Resource root) {
        List<String> result = new ArrayList<>();
        if (root == null) {
            return result;
        }

        Page page = root.adaptTo(Page.class);
        if (page == null) {
            log.warn("Resource không phải là Page: {}", root.getPath());
            return result;
        }

        // deep = true → duyệt toàn bộ cây con (không gồm chính root).
        // PageFilter(true, true) = gồm cả page invalid + hidden → khớp với "tất cả cq:Page".
        Iterator<Page> children = page.listChildren(new PageFilter(true, true), true);
        while (children.hasNext()) {
            result.add(children.next().getPath());
        }
        return result;
    }

    // ---------------------------------------------------------------------
    // Cách 3: Java Stream traversal (đệ quy, lazy) — nhanh & nhẹ nhất
    // ---------------------------------------------------------------------
    @Override
    public List<String> listUsingStreamTraversal(Resource root) {
        if (root == null) {
            return new ArrayList<>();
        }
        // Chỉ traverse descendant (không gồm root) và chỉ lấy node cq:Page.
        return traverseDescendants(root)
                .filter(this::isPage)
                .map(Resource::getPath)
                .collect(Collectors.toList());
    }

    /**
     * Đệ quy traverse các descendant của {@code parent} theo kiểu lazy stream,
     * KHÔNG bao gồm chính {@code parent}, và không đi sâu vào node {@code jcr:content}.
     */
    private Stream<Resource> traverseDescendants(Resource parent) {
        Stream<Resource> children = StreamSupport.stream(parent.getChildren().spliterator(), false)
                .filter(this::shouldFollow);
        return children.flatMap(child ->
                Stream.concat(Stream.of(child), traverseDescendants(child)));
    }

    /** Không đi sâu vào node jcr:content (đó là nội dung của page, không phải page con). */
    private boolean shouldFollow(Resource resource) {
        return !JcrConstants.JCR_CONTENT.equals(resource.getName());
    }

    /** Một resource là page khi jcr:primaryType = cq:Page. */
    private boolean isPage(Resource resource) {
        return NameConstants.NT_PAGE.equals(
                resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class));
    }
}
