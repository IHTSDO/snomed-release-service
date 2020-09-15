package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.data.domain.*;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class ProductDAOImpl extends EntityDAOImpl<Product> implements ProductDAO {

    public ProductDAOImpl() {
        super(Product.class);
    }

    @Override
    public Page<Product> findAll(Set<FilterOption> filterOptions, Pageable pageable) {
        return findAll(null, filterOptions, pageable);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<Product> findAll(String releaseCenterBusinessKey, Set<FilterOption> filterOptions, Pageable pageable) {

        String filter = "";
        if (filterOptions != null && filterOptions.contains(FilterOption.INCLUDE_REMOVED)) {
            filter += " ( removed = 'N' or removed is null) ";
        }
        if (releaseCenterBusinessKey != null) {
            if (!filter.isEmpty()) {
                filter += " and ";
            }
            filter += " releaseCenter.businessKey = :releaseCenterBusinessKey ";
        }
        Query query = getCurrentSession().createQuery(
                "select product " +
                        "from Product product " +
                        "join product.releaseCenter releaseCenter " +
                        "where " +
                        filter +
                        "order by product.id ");
        if (releaseCenterBusinessKey != null) {
            query.setString("releaseCenterBusinessKey", releaseCenterBusinessKey);
        }

        // Pagination
        List<Product> products = query.list();
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int fromIndex  = 0;
        int toIndex  = 0;
        if (products.size() > 0) {
            fromIndex  = pageSize * pageNumber;
            toIndex  = (fromIndex + pageSize) > products.size() ? products.size() : fromIndex + pageSize;
            return new PageImpl(products.subList(fromIndex, toIndex), pageable, products.size());
        } else {
            return new PageImpl(Collections.emptyList(), pageable, 0);
        }
    }

    @Override
    public Product find(Long id) {
        Query query = getCurrentSession().createQuery(
                "select product " +
                        "from Product product " +
                        "where product.id = :productId " +
                        "order by product.id ");
        query.setLong("productId", id);
        return (Product) query.uniqueResult();
    }

    /**
     * Look for a specific product where the id (primary key) is not necessarily known
     */
    @Override
    public Product find(String releaseCenterKey,
                        String productKey) {
        Query query = getCurrentSession().createQuery(
                "select product " +
                        "from Product product " +
                        "join product.releaseCenter releaseCenter " +
                        "where releaseCenter.businessKey = :releaseCenterBusinessKey " +
                        "and product.businessKey = :productBusinessKey ");
        query.setString("releaseCenterBusinessKey", releaseCenterKey);
        query.setString("productBusinessKey", productKey);
        return (Product) query.uniqueResult();
    }
}
