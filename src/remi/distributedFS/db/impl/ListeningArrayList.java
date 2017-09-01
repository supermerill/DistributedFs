package remi.distributedFS.db.impl;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import remi.distributedFS.util.Ref;

public class ListeningArrayList<E> extends AbstractList<E>{
	
	final ArrayList<E> lst;
	final Ref<Boolean> isDirty;
	
	public ListeningArrayList(Ref<Boolean> isDirty){
		lst = new ArrayList<>();
		this.isDirty = isDirty;
	}
	
	public ListeningArrayList(List<?extends E> lst, Ref<Boolean> isDirty){
		this.lst = new ArrayList<>(lst);
		this.isDirty = isDirty;
	}

	@Override
	public E get(int index) {
		return lst.get(index);
	}

	@Override
	public int size() {
		return lst.size();
	}

    /**
     * {@inheritDoc}
     *
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public E set(int index, E element) {
    	this.isDirty.set(true);
    	return lst.set(index, element);
    }

    /**
     * {@inheritDoc}
     *
     *
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public void add(int index, E element) {
    	this.isDirty.set(true);
    	lst.add(index, element);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public E remove(int index) {
    	this.isDirty.set(true);
    	return lst.remove(index);
    }
}
