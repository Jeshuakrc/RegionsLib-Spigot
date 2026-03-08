package com.kntrel.mc.regionLib.util;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Grid implements Iterable<Grid.Cell> {

    private final int minX_, minZ_, length_, depth_, size_;
    private final World world_;

    public Grid(int minX, int minZ, int maxX, int maxZ, World world) {
        this.minX_ = minX;
        this.minZ_ = minZ;
        this.length_ = maxX - minX + 1;
        this.depth_ = maxZ - minZ + 1;
        this.size_ = this.length_ * this.depth_;
        this.world_ = world;
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator() {
        return new GridIterator();
    }

    public Stream<Cell> cellStream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    public Set<Cell> cellSet() {
        return this.cellStream().collect(Collectors.toSet());
    }

    public boolean contains(Cell cell) {
        return cell.world().getUID().equals(this.world_.getUID())
                && cell.x() >= this.minX_ && cell.x() < this.minX_ + this.length_
                && cell.z() >= this.minZ_ && cell.z() < this.minZ_ + this.depth_;
    }

    //SUBTYPES
    private class GridIterator implements Iterator<Cell> {
        private int pos_ = 0;

        @Override
        public boolean hasNext() {
            return this.pos_ < Grid.this.size_;
        }

        @Override
        public Cell next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            int x = this.pos_ % Grid.this.length_,
                    z = this.pos_ / Grid.this.length_;
            this.pos_++;
            return new Cell(Grid.this.minX_ + x, Grid.this.minZ_ + z, Grid.this.world_);
        }
    }

    public static class CellSize {
        //CONSTANTS
        public static final CellSize
            SIZE_16 = new CellSize(4),
            SIZE_32 = new CellSize(5),
            SIZE_64 = new CellSize(6),
            SIZE_12 = new CellSize(7),
            SIZE_25 = new CellSize(8);

        //FACTORY
        public static CellSize ofTwoToThe(int power) {
            return new CellSize(power);
        }
        public static CellSize of(int size) {
            int shift = Integer.numberOfTrailingZeros(size);
            if ((1 << shift) != size) {
                throw new IllegalArgumentException("Size must be a power of two. Provided: " + size);
            }
            return new CellSize(shift);
        }

        //FIELDS
        private final int shift_;

        //CONSTRUCTORS
        private CellSize(int shift) { this.shift_ = shift; }

        //GETTERS
        public int shiftBy() { return this.shift_; }
        public int getSize() { return 1 << this.shift_; }

        //IMPLEMENTATION
        @Override public int hashCode() {
            return Integer.hashCode(this.shift_);
        }
        @Override public boolean equals(Object o) {
            if (o == null) { return false; }
            if (o == this) { return true; }
            if (!(o instanceof CellSize other)) { return false; }
            return this.shift_ == other.shift_;
        }
    }

    public record Cell(int x, int z, World world) {
        @Override public boolean equals(Object o) {
            if (o == null) { return false; }
            if (o == this) { return true; }
            if (!(o instanceof Cell other)) { return false; }
            return this.x == other.x && this.z == other.z && this.world.getUID().equals(other.world.getUID());
        }
        @Override public int hashCode() {
            return Objects.hash(this.x, this.z, this.world.getUID());
        }
    }
}
