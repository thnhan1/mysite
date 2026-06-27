package com.mysite.core.models;

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import com.mysite.core.services.ListingAEMChildrendService;

/**
 * Model demo so sánh 3 cách liệt kê descendant page, bắt đầu từ {@value #ROOT_PATH}.
 *
 * <p>Cả 3 cách dùng <b>chung 1 gốc</b> ({@value #ROOT_PATH}) và <b>chung 1 tiêu chí</b>
 * (tất cả {@code cq:Page} con/cháu, không gồm root) nên cho ra cùng tập kết quả,
 * chỉ khác nhau về hiệu năng.
 *
 * <p>Mỗi cách trả về một {@link Result} gồm danh sách path, số lượng và thời gian
 * thực thi (ms) để dễ so sánh trực tiếp trên HTL.
 *
 * @see ListingAEMChildrendService
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PageListingModel {

    /** Gốc cố định để traverse. */
    private static final String ROOT_PATH = "/content/mysite";

    @OSGiService
    private ListingAEMChildrendService listingPageService;

    @SlingObject
    private Resource resource;

    private String rootPath;
    private Result jcrQuery = Result.empty();
    private Result pageManager = Result.empty();
    private Result streamTraversal = Result.empty();

    @PostConstruct
    protected void init() {
        if (listingPageService == null || resource == null) {
            return;
        }

        ResourceResolver resolver = resource.getResourceResolver();
        Resource rootResource = resolver.getResource(ROOT_PATH);
        if (rootResource == null) {
            return;
        }
        rootPath = rootResource.getPath();

        jcrQuery = measure(() -> listingPageService.listUsingJcrQuery(rootResource));
        pageManager = measure(() -> listingPageService.listUsingPageManager(rootResource));
        streamTraversal = measure(() -> listingPageService.listUsingStreamTraversal(rootResource));
    }

    /** Gọi supplier, đo thời gian thực thi và gói kết quả lại. */
    private Result measure(java.util.function.Supplier<List<String>> supplier) {
        long start = System.nanoTime();
        List<String> paths = supplier.get();
        double durationMs = (System.nanoTime() - start) / 1_000_000.0;
        return new Result(paths, durationMs);
    }

    public String getRootPath() {
        return rootPath;
    }

    public Result getJcrQuery() {
        return jcrQuery;
    }

    public Result getPageManager() {
        return pageManager;
    }

    public Result getStreamTraversal() {
        return streamTraversal;
    }

    /** Kết quả của một cách liệt kê: danh sách path, số lượng và thời gian (ms). */
    public static final class Result {

        private final List<String> paths;
        private final double durationMs;

        private Result(List<String> paths, double durationMs) {
            this.paths = paths != null ? paths : Collections.emptyList();
            this.durationMs = durationMs;
        }

        static Result empty() {
            return new Result(Collections.emptyList(), 0d);
        }

        public List<String> getPaths() {
            return paths;
        }

        public int getCount() {
            return paths.size();
        }

        public double getDurationMs() {
            return durationMs;
        }
    }
}
