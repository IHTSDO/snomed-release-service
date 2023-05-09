package org.ihtsdo.buildcloud.core.dao;

import org.hibernate.query.Query;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

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
	public Page<Product> findAll(String releaseCenterKey, Set<FilterOption> filterOptions, Pageable pageable) {
		String filter = "product.visibility = 'Y' ";
		if (filterOptions != null && filterOptions.contains(FilterOption.INCLUDE_REMOVED)) {
			if (!filter.isEmpty()) {
				filter += " and ";
			}
			filter += " ( removed = 'N' or removed is null) ";
		}
		if (filterOptions == null || !filterOptions.contains(FilterOption.INCLUDE_LEGACY)) {
			if (!filter.isEmpty()) {
				filter += " and ";
			}
			filter += " product.isLegacyProduct = 'N' ";
		}
		if (releaseCenterKey != null) {
			if (!filter.isEmpty()) {
				filter += " and ";
			}
			filter += " releaseCenter.businessKey = :releaseCenterBusinessKey ";
		}
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
		if (pageable.getSort() != null && !pageable.getSort().isEmpty()) {
			Sort.Order order = pageable.getSort().iterator().next();
			queryString += "product." + order.getProperty().toLowerCase() + " " + order.getDirection().toString();
		} else {
			queryString += "product.id DESC";
		}


		Query query = getCurrentSession().createQuery(queryString);
		query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
		query.setMaxResults(pageable.getPageSize());

		Query queryTotal = getCurrentSession().createQuery(queryTotalString);
		if (releaseCenterKey != null) {
			query.setParameter("releaseCenterBusinessKey", releaseCenterKey);
			queryTotal.setParameter("releaseCenterBusinessKey", releaseCenterKey);
		}

		return new PageImpl(query.list(), pageable, (long) queryTotal.uniqueResult());
	}

	@Override
	public Page<Product> findHiddenProducts(String releaseCenterKey, Pageable pageable) {
		String filter = "(product.isLegacyProduct = 'Y' or product.visibility = 'N') and releaseCenter.businessKey = :releaseCenterBusinessKey ";
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
		if (pageable.getSort() != null && !pageable.getSort().isEmpty()) {
			Sort.Order order = pageable.getSort().iterator().next();
			queryString += "product." + order.getProperty().toLowerCase() + " " + order.getDirection().toString();
		} else {
			queryString += "product.id DESC";
		}


		Query query = getCurrentSession().createQuery(queryString);
		query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
		query.setMaxResults(pageable.getPageSize());

		Query queryTotal = getCurrentSession().createQuery(queryTotalString);
		if (releaseCenterKey != null) {
			query.setParameter("releaseCenterBusinessKey", releaseCenterKey);
			queryTotal.setParameter("releaseCenterBusinessKey", releaseCenterKey);
		}

		return new PageImpl(query.list(), pageable, (long) queryTotal.uniqueResult());
	}

	@Override
	public Product find(Long id) {
		Query query = getCurrentSession().createQuery(
				"select product " +
						"from Product product " +
						"where product.id = :productId " +
						"order by product.id ");
		query.setParameter("productId", id);
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
		query.setParameter("releaseCenterBusinessKey", releaseCenterKey);
		query.setParameter("productBusinessKey", productKey);
		return (Product) query.uniqueResult();
	}

	@Override
	public Product find(String productKey) {
		Query query = getCurrentSession().createQuery(
				"select product " +
						"from Product product " +
						"where product.businessKey = :productBusinessKey ");
		query.setParameter("productBusinessKey", productKey);
		return (Product) query.uniqueResult();
	}
}
