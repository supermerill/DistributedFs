package remi.distributedFS.db.impl;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import remi.distributedFS.util.Ref;

public class ListeningArrayList<E> extends AbstractList<E>{
	
	final ArrayList<E> lst;
	final Consumer<E> add;
	final Consumer<E> rem;
	
	public ListeningArrayList( Consumer<E> add, Consumer<E> remove){
		lst = new ArrayList<>();
		this.add = add;
		this.rem = remove;
	}
	
	public ListeningArrayList(List<?extends E> lst, Consumer<E> add, Consumer<E> remove){
		this.lst = new ArrayList<>(lst);
		this.add = add;
		this.rem = remove;
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
    	E elemP = lst.set(index, element);
    	if(elemP != element){
    		if(elemP != null) this.rem.accept(elemP);
    		if(element != null) this.add.accept(element);
    	}
    	return elemP;
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
    	lst.add(index, element);
    	this.add.accept(element);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public E remove(int index) {
    	E elem = lst.remove(index);
    	this.rem.accept(elem);
    	return elem;
    }
}
