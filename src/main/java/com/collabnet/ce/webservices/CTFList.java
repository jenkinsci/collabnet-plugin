package com.collabnet.ce.webservices;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Glorified {@link ArrayList} that supports a look-up by the title.
 * @author Kohsuke Kawaguchi
 */
public class CTFList<T extends ObjectWithTitle> extends ArrayList<T> {
    public CTFList() {
    }

    public CTFList(Collection<? extends T> c) {
        super(c);
    }

    /**
     * Looks up the object by its title.
     */
    public T byTitle(String title) {
        for (T t : this) {
            if (t.getTitle().equals(title))
                return t;
        }
        return null;
    }

    /**
     * Looks up the object by its ID.
     */
    public T byId(String id) {
        for (T t : this) {
            if (t.getId().equals(id))
                return t;
        }
        return null;
    }

    /**
     * Gets the view of this list where titles are returned instead of {@code T}.
     */
    public List<String> getTitles() {
        return new AbstractList<String>() {
            @Override
            public String get(int index) {
                return CTFList.this.get(index).getTitle();
            }

            @Override
            public int size() {
                return CTFList.this.size();
            }
        };
    }
}
