package com.easyview.common;

import java.io.Serializable;

/* loaded from: classes.dex */
public final class Pair<A, B> implements Serializable {
    private static final long serialVersionUID = 1;
    public final A first;
    public final B second;

    private Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<>(first, second);
    }

    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Pair other = (Pair) obj;
        if (this.first == other.first || (this.first != null && this.first.equals(other.first))) {
            return this.second == other.second || (this.second != null && this.second.equals(other.second));
        }
        return false;
    }

    public int hashCode() {
        int hash = (this.first != null ? this.first.hashCode() : 0) + 259;
        return (hash * 37) + (this.second != null ? this.second.hashCode() : 0);
    }

    public String toString() {
        return String.format("Pair[%s,%s]", this.first, this.second);
    }
}
