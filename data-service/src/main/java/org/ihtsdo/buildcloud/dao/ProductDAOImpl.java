package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public class ProductDAOImpl extends EntityDAOImpl<Product> implements ProductDAO {

	protected ProductDAOImpl() {
		super(Product.class);
	}

	@Override
	public Product find(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, User user) {
		Query query = getCurrentSession().createQuery(
				"select product " +
						"from ReleaseCenterMembership membership " +
						"join membership.releaseCenter releaseCenter " +
						"join releaseCenter.extensions extension " +
						"join extension.products product " +
						"where membership.user = :user " +
						"and releaseCenter.businessKey = :releaseCenterBusinessKey " +
						"and extension.businessKey = :extensionBusinessKey " +
						"and product.businessKey = :productBusinessKey " +
						"order by product.id ");
		query.setEntity("user", user);
		query.setString("releaseCenterBusinessKey", releaseCenterBusinessKey);
		query.setString("extensionBusinessKey", extensionBusinessKey);
		query.setString("productBusinessKey", productBusinessKey);
		return (Product) query.uniqueResult();
	}

}
