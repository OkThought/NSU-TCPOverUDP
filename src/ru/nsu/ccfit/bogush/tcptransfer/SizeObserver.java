package ru.nsu.ccfit.bogush.tcptransfer;

public interface SizeObserver {
	long getTotalSize();
	long getCurrentSize();
}