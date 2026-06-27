package com.mysite.core.services;

import org.apache.sling.api.resource.Resource;

import java.util.List;

/**
 * Service liệt kê tất cả descendant page (con/cháu) của một resource gốc.
 *
 * <p>Cung cấp 3 cách triển khai để so sánh hiệu năng và đặc tính:
 * <ul>
 *   <li>{@link #listUsingJcrQuery(Resource)} — QueryBuilder (JCR Query)</li>
 *   <li>{@link #listUsingPageManager(Resource)} — Page.listChildren + PageFilter</li>
 *   <li>{@link #listUsingStreamTraversal(Resource)} — Java Stream traversal (đệ quy lazy)</li>
 * </ul>
 *
 * <p>Khuyến nghị của AEM:
 * <ul>
 *   <li><b>JCR Query</b> — tốn tài nguyên nhất; chỉ dùng cho end-user search,
 *       KHÔNG dùng cho render request (navigation, đếm content).</li>
 *   <li><b>Page.listChildren</b> — đơn giản, ít phức tạp; nhưng chậm hơn stream
 *       và đôi khi thiếu kết quả.</li>
 *   <li><b>Stream traversal</b> — nhanh nhất, tốn ít tài nguyên nhất; ưu tiên dùng
 *       khi traverse nhiều cấp nhờ lazy evaluation.</li>
 * </ul>
 */
public interface ListingAEMChildrendService {

    /**
     * Liệt kê descendant page bằng QueryBuilder (JCR Query).
     *
     * @param root resource gốc để bắt đầu tìm
     * @return danh sách path của các page tìm được
     */
    List<String> listUsingJcrQuery(Resource root);

    /**
     * Liệt kê descendant page bằng {@code Page.listChildren} với {@code PageFilter} (deep).
     *
     * @param root resource gốc để bắt đầu duyệt
     * @return danh sách path của các page con/cháu
     */
    List<String> listUsingPageManager(Resource root);

    /**
     * Liệt kê descendant page bằng Java Stream traversal (đệ quy, lazy evaluation)., **ƯU tiên**
     *
     * @param root resource gốc để bắt đầu duyệt
     * @return danh sách path của các page con/cháu
     */
    List<String> listUsingStreamTraversal(Resource root);
}
