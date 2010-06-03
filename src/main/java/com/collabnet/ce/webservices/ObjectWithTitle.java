package com.collabnet.ce.webservices;

/**
 * All {@link CTFItem}s have a title, but there are some that has titles
 * that are not {@link CTFItem}s.
 *
 * @author Kohsuke Kawaguchi
 */
public interface ObjectWithTitle {
    String getTitle();
    String getId();
}
