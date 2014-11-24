package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;
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
	public List<Product> findAll(Set<FilterOption> filterOptions, User user) {
		return findAll(null, filterOptions, user);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Product> findAll(String releaseCenterBusinessKey, Set<FilterOption> filterOptions, User user) {

		String filter = "";
		if (filterOptions != null && filterOptions.contains(FilterOption.INCLUDE_REMOVED)) {
			filter += " and ( removed = 'N' or removed is null) ";
		}
		if (releaseCenterBusinessKey != null) {
			filter += " and releaseCenter.businessKey = :releaseCenterBusinessKey ";
		}
		Query query = getCurrentSession().createQuery(
				"select product " +
						"from ReleaseCenterMembership membership " +
						"join membership.releaseCenter releaseCenter " +
						"join releaseCenter.products product " +
						"where membership.user = :user " +
						filter +
						"order by product.id ");
		query.setEntity("user", user);

		if (releaseCenterBusinessKey != null) {
			query.setString("releaseCenterBusinessKey", releaseCenterBusinessKey);
		}
		return query.list();
	}

	@Override
	public Product find(Long id, User user) {
		Query query = getCurrentSession().createQuery(
				"select product " +
						"from ReleaseCenterMembership membership " +
						"join membership.releaseCenter releaseCenter " +
						"join releaseCenter.products product " +
						"where membership.user = :user " +
						"and product.id = :productId " +
						"order by product.id ");
		query.setEntity("user", user);
		query.setLong("productId", id);
		return (Product) query.uniqueResult();
	}

	/**
	 * Look for a specific product where the id (primary key) is not necessarily known
	 */
	@Override
	public Product find(String releaseCenterKey,
			String productKey, User user) {
		Query query = getCurrentSession().createQuery(
				"select product " +
						"from ReleaseCenterMembership membership " +
						"join membership.releaseCenter releaseCenter " +
						"join releaseCenter.products product " +
						"where membership.user = :user " +
						"and releaseCenter.businessKey = :releaseCenterBusinessKey " +
						"and product.businessKey = :productBusinessKey ");
		query.setEntity("user", user);
		query.setString("releaseCenterBusinessKey", releaseCenterKey);
		query.setString("productBusinessKey", productKey);
		return (Product) query.uniqueResult();
	}

}
