package org.ihtsdo.buildcloud.core.dao;

import org.hibernate.query.Query;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
	@SuppressWarnings("unchecked")
	public Page<Product> findAll(String releaseCenterBusinessKey, Set<FilterOption> filterOptions, Pageable pageable) {
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
		if (releaseCenterBusinessKey != null) {
			if (!filter.isEmpty()) {
				filter += " and ";
			}
			filter += " releaseCenter.businessKey = :releaseCenterBusinessKey ";
		}
		String fromClause = "from Product product join product.releaseCenter releaseCenter ";
		Query query = getCurrentSession().createQuery(
				"select product " +
						fromClause +
						"where " +
						filter +
						"order by product.id DESC");
		Query queryTotal = getCurrentSession().createQuery(
				"select count(product.id) " +
						fromClause +
						"where " +
						filter);
		if (releaseCenterBusinessKey != null) {
			query.setParameter("releaseCenterBusinessKey", releaseCenterBusinessKey);
			queryTotal.setParameter("releaseCenterBusinessKey", releaseCenterBusinessKey);
		}

		query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
		query.setMaxResults(pageable.getPageSize());

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
}
