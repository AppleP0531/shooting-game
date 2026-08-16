package com.game.shooting_game.utils;

import lombok.Data;

@Data
public class Anchor {
    public static final Anchor CENTER = new Anchor(H.CENTER, V.CENTER);

    public enum H {
        LEFT(0),
        CENTER(0.5),
        RIGHT(1);

        private final double rate;

        H(double rate) {
            this.rate = rate;
        }
    }

    public enum V {
        TOP(0),
        CENTER(0.5),
        BOTTOM(1);

        private final double rate;

        V(double rate) {
            this.rate = rate;
        }
    }

    public final H horizontal;
    public final V vertical;

    public Anchor(Anchor anchor) {
        this.horizontal = anchor.horizontal;
        this.vertical = anchor.vertical;
    }

    public Anchor(H h, V v) {
        this.horizontal = h;
        this.vertical = v;
    }

    public Vector2 getValue() {
        return new Vector2(horizontal.rate, vertical.rate);
    }
}
