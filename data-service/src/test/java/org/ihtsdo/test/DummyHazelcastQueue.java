package org.ihtsdo.test;

import com.hazelcast.core.IQueue;
import com.hazelcast.core.ItemListener;
import com.hazelcast.monitor.LocalQueueStats;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class DummyHazelcastQueue<T> implements IQueue<T> {


	@Override
	public boolean add(T t) {
		return false;
	}

	@Override
	public boolean offer(T t) {
		return false;
	}

	@Override
	public void put(T t) throws InterruptedException {

	}

	@Override
	public boolean offer(T t, long l, TimeUnit timeUnit) throws InterruptedException {
		return false;
	}

	@Override
	public T take() throws InterruptedException {
		return null;
	}

	@Override
	public T poll(long l, TimeUnit timeUnit) throws InterruptedException {
		return null;
	}

	@Override
	public int remainingCapacity() {
		return 0;
	}

	@Override
	public boolean remove(Object o) {
		return false;
	}

	@Override
	public boolean contains(Object o) {
		return false;
	}

	@Override
	public int drainTo(Collection<? super T> objects) {
		return 0;
	}

	@Override
	public int drainTo(Collection<? super T> objects, int i) {
		return 0;
	}

	@Override
	public T remove() {
		return null;
	}

	@Override
	public T poll() {
		return null;
	}

	@Override
	public T element() {
		return null;
	}

	@Override
	public T peek() {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public Iterator<T> iterator() {
		return null;
	}

	@Override
	public Object[] toArray() {
		return new Object[0];
	}

	@Override
	public <T1> T1[] toArray(T1[] t1s) {
		return null;
	}

	@Override
	public boolean containsAll(Collection<?> objects) {
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends T> ts) {
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> objects) {
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> objects) {
		return false;
	}

	@Override
	public void clear() {

	}

	@Override
	public LocalQueueStats getLocalQueueStats() {
		return null;
	}

	@Override
	public String addItemListener(ItemListener<T> tItemListener, boolean b) {
		return null;
	}

	@Override
	public boolean removeItemListener(String s) {
		return false;
	}

	@Override
	public Object getId() {
		return null;
	}

	@Override
	public String getPartitionKey() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getServiceName() {
		return null;
	}

	@Override
	public void destroy() {

	}
}
