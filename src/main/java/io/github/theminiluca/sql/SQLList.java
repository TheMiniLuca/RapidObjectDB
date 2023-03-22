package io.github.theminiluca.sql;

import java.io.Serializable;
import java.util.*;

public class SQLList<T extends Values> implements List<T>, Serializable {

    private final int sizeOfValue;
    private final ArrayList<T> root;
    protected List<Integer[]> actionQueue = new ArrayList<>();

    public SQLList(int sizeOfValue) {
        this(sizeOfValue, new ArrayList<>());
    }

    public SQLList(int sizeOfValue, ArrayList<T> list) {
        root = list;
        this.sizeOfValue = sizeOfValue;
        if(sizeOfValue < 1) throw new IllegalArgumentException("size of value needs to be bigger than 0");
    }


    @Override
    public int size() {
        return root.size();
    }

    @Override
    public boolean isEmpty() {
        return root.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return root.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return root.iterator();
    }

    @Override
    public Object[] toArray() {
        return root.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return root.toArray(a);
    }

    @Override
    public boolean add(T t) {
        if(t.getLength() != sizeOfValue) throw new IllegalArgumentException("Element's size need to be same as SQLList's value size.");
        boolean val = root.add(t);
        actionQueue.add(new Integer[]{1, root.indexOf(t)});
        return val;
    }

    @Override
    public boolean remove(Object o) {
        actionQueue.add(new Integer[]{-1, root.indexOf(o)});
        return root.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return root.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        c.forEach((e) -> {
            if(e.getLength() != sizeOfValue) throw new IllegalArgumentException("Element's size need to be same as SQLList's value size.");
            actionQueue.add(new Integer[]{1, root.indexOf(e)});
        });
        return root.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        c.forEach((e) -> {
            if(e.getLength() != sizeOfValue) throw new IllegalArgumentException("Element's size need to be same as SQLList's value size.");
            actionQueue.add(new Integer[]{1, root.indexOf(e)});
        });
        return root.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        c.forEach((e) -> actionQueue.add(new Integer[]{-1, root.indexOf(e)}));
        return root.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not Supported");
//        return root.retainAll(c);
    }

    @Override
    public void clear() {
        actionQueue.add(new Integer[]{-2, -2});
        root.clear();
    }

    @Override
    public T get(int index) {
        return root.get(index);
    }

    @Override
    public T set(int index, T element) {
        if(element.getLength() != sizeOfValue) throw new IllegalArgumentException("Element's size need to be same as SQLList's value size.");
        actionQueue.add(new Integer[]{0, index});
        return root.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        if(element.getLength() != sizeOfValue) throw new IllegalArgumentException("Element's size need to be same as SQLList's value size.");
        root.add(index, element);
        actionQueue.add(new Integer[]{1, root.indexOf(element)});
    }

    @Override
    public T remove(int index) {
        actionQueue.add(new Integer[]{-1, index});
        return root.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return root.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return root.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return root.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return root.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Not Supported");
//        return root.subList(fromIndex, toIndex);
    }
}