package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ProductDAOImpl extends EntityDAOImpl<Product> implements ProductDAO {

	@Override
	public Product find(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select product " +
				"from ReleaseCenterMembership membership " +
				"join membership.releaseCenter releaseCenter " +
				"join releaseCenter.extensions extension " +
				"join extension.products product " +
				"where membership.user.oauthId = :oauthId " +
				"and releaseCenter.businessKey = :releaseCenterBusinessKey " +
				"and extension.businessKey = :extensionBusinessKey " +
				"and product.businessKey = :productBusinessKey " +
				"order by product.id ");
		query.setString("oauthId", authenticatedId);
		query.setString("releaseCenterBusinessKey", releaseCenterBusinessKey);
		query.setString("extensionBusinessKey", extensionBusinessKey);
		query.setString("productBusinessKey", productBusinessKey);
		return (Product) query.uniqueResult();
	}

}
