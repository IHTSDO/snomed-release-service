package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public class ProductDAOImpl extends EntityDAOImpl<Product> implements ProductDAO {

	public ProductDAOImpl() {
		super(Product.class);
	}

	@Override
	public List<Product> findAll(Set<FilterOption> filterOptions) {
		return findAll(null, filterOptions);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Product> findAll(String releaseCenterBusinessKey, Set<FilterOption> filterOptions) {

		String filter = "";
		if (filterOptions != null && filterOptions.contains(FilterOption.INCLUDE_REMOVED)) {
			filter += " ( removed = 'N' or removed is null) ";
		}
		if (releaseCenterBusinessKey != null) {
			if(!filter.isEmpty()) {
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
		return query.list();
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
