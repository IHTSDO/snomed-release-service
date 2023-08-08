package org.ihtsdo.buildcloud.core.dao;

import org.hibernate.query.Query;
import org.ihtsdo.buildcloud.core.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationDaoImpl extends EntityDAOImpl<Notification> implements NotificationDao {

	protected NotificationDaoImpl() {
		super(Notification.class);
	}

	@Override
	public Page<Notification> findAll(String recipient, PageRequest pageRequest) {
		Query<Notification> query = getCurrentSession().createQuery(
				"select notification " +
						"from Notification notification " +
						"where notification.recipient = :recipient " +
						"order by notification.createdDate DESC", Notification.class);
		query.setParameter("recipient", recipient);
		query.setFirstResult(pageRequest.getPageNumber() * pageRequest.getPageSize());
		query.setMaxResults(pageRequest.getPageSize());

		Query<Long> queryTotal = getCurrentSession().createQuery(
				"select count(notification.id) " +
						"from Notification notification " +
						"where notification.recipient = :recipient", Long.class);
		queryTotal.setParameter("recipient", recipient);


		return new PageImpl<>(query.list(), pageRequest, queryTotal.uniqueResult());
	}

	@Override
	public Long countUnreadNotifications(String recipient) {
		Query<Long> queryTotal = getCurrentSession().createQuery(
				"select count(notification.id) " +
						"from Notification notification " +
						"where notification.recipient = :recipient and notification.read = 'N'", Long.class);
		queryTotal.setParameter("recipient", recipient);

		return queryTotal.uniqueResult();
	}

	@Override
	public List<Notification> findByRecipient(String recipient) {
		Query<Notification> query = getCurrentSession().createQuery(
				"select notification " +
						"from Notification notification " +
						"where notification.recipient = :recipient", Notification.class);
		query.setParameter("recipient", recipient);
		return query.list();
	}

	@Override
	public List<Notification> findByIds(List<Long> notificationIds) {
		Query<Notification> query = getCurrentSession().createQuery(
				"select notification " +
						"from Notification notification " +
						"where notification.id in (:notificationIds)", Notification.class);
		query.setParameter("notificationIds", notificationIds);
		return query.list();
	}
}
