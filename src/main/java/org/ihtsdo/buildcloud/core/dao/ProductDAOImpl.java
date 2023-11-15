package org.ihtsdo.buildcloud.core.dao;

import org.hibernate.query.Query;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

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
	public Page<Product> findAll(String releaseCenterKey, Set<FilterOption> filterOptions, Pageable pageable) {
		String filter = constructFilter(releaseCenterKey, filterOptions);
		String fromClause = "from Product product join product.releaseCenter releaseCenter ";
		String queryString = "select product " +
				fromClause +
				"where " +
				filter +
				"order by ";
		String queryTotalString = "select count(product.id) " +
				fromClause +
				"where " +
				filter;
		if (!pageable.getSort().isEmpty()) {
			Sort.Order order = pageable.getSort().iterator().next();
			queryString += "product." + order.getProperty().toLowerCase() + " " + order.getDirection();
		} else {
			queryString += "product.id DESC";
		}


		Query<Product> query = getCurrentSession().createQuery(queryString, Product.class);
		query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
		query.setMaxResults(pageable.getPageSize());

		Query<Long> queryTotal = getCurrentSession().createQuery(queryTotalString, Long.class);
		if (releaseCenterKey != null) {
			query.setParameter("releaseCenterBusinessKey", releaseCenterKey);
			queryTotal.setParameter("releaseCenterBusinessKey", releaseCenterKey);
		}

		return new PageImpl<>(query.list(), pageable, queryTotal.uniqueResult());
	}

	private static String constructFilter(String releaseCenterKey, Set<FilterOption> filterOptions) {
		String filter = "product.visibility = TRUE ";
		if (filterOptions != null && filterOptions.contains(FilterOption.INCLUDE_REMOVED)) {
            filter += " and ";
            filter += " ( removed = FALSE or removed is null) ";
		}
		if (filterOptions == null || !filterOptions.contains(FilterOption.INCLUDE_LEGACY)) {
            filter += " and ";
            filter += " product.isLegacyProduct = FALSE ";
		}
		if (releaseCenterKey != null) {
            filter += " and ";
            filter += " releaseCenter.businessKey = :releaseCenterBusinessKey ";
		}
		return filter;
	}

	@Override
	public Page<Product> findHiddenProducts(String releaseCenterKey, Pageable pageable) {
		String filter = "(product.isLegacyProduct = TRUE or product.visibility = FALSE) and releaseCenter.businessKey = :releaseCenterBusinessKey ";
		String fromClause = "from Product product join product.releaseCenter releaseCenter ";
		String queryString = "select product " +
				fromClause +
				"where " +
				filter +
				"order by ";
		String queryTotalString = "select count(product.id) " +
				fromClause +
				"where " +
				filter;
		if (!pageable.getSort().isEmpty()) {
			Sort.Order order = pageable.getSort().iterator().next();
			queryString += "product." + order.getProperty().toLowerCase() + " " + order.getDirection();
		} else {
			queryString += "product.id DESC";
		}


		Query<Product> query = getCurrentSession().createQuery(queryString, Product.class);
		query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
		query.setMaxResults(pageable.getPageSize());

		Query<Long> queryTotal = getCurrentSession().createQuery(queryTotalString, Long.class);
		if (releaseCenterKey != null) {
			query.setParameter("releaseCenterBusinessKey", releaseCenterKey);
			queryTotal.setParameter("releaseCenterBusinessKey", releaseCenterKey);
		}

		return new PageImpl<>(query.list(), pageable, queryTotal.uniqueResult());
	}

	@Override
	public Product find(Long id) {
		Query<Product> query = getCurrentSession().createQuery(
				"select product " +
						"from Product product " +
						"where product.id = :productId " +
						"order by product.id ", Product.class);
		query.setParameter("productId", id);
		return query.uniqueResult();
	}

	/**
	 * Look for a specific product where the id (primary key) is not necessarily known
	 */
	@Override
	public Product find(String releaseCenterKey,
						String productKey) {
		Query<Product> query = getCurrentSession().createQuery(
				"select product " +
						"from Product product " +
						"join product.releaseCenter releaseCenter " +
						"where releaseCenter.businessKey = :releaseCenterBusinessKey " +
						"and product.businessKey = :productBusinessKey ", Product.class);
		query.setParameter("releaseCenterBusinessKey", releaseCenterKey);
		query.setParameter("productBusinessKey", productKey);
		return query.uniqueResult();
	}

	@Override
	public Product find(String productKey) {
		Query<Product> query = getCurrentSession().createQuery(
				"select product " +
						"from Product product " +
						"where product.businessKey = :productBusinessKey ", Product.class);
		query.setParameter("productBusinessKey", productKey);
		return query.uniqueResult();
	}
}
