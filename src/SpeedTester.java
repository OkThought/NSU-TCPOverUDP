public class SpeedTester extends Thread implements StopListener {
	private static final int STATUS_BAR_SIZE = 32;
	private static final double KB = 1024;
	private static final double MB = 2 << 20;
	private static final long SECOND = 1000;
	private long total;
	private long previous = 0;
	private long period;
	private SizeObserver sizeObserver;

	public SpeedTester(SizeObserver sizeObserver, long period) {
		this.sizeObserver = sizeObserver;
		this.period = period;
	}

	@Override
	public synchronized void start() {
		super.start();
	}

	private void printProgress(double percent) {
		System.out.append('[');
		int hashNum = (int) (percent * STATUS_BAR_SIZE);
		if (hashNum > STATUS_BAR_SIZE) throw new RuntimeException("hashNum: " + hashNum + " percent: " + percent);
		for (int i = 0; i < hashNum && i < STATUS_BAR_SIZE; i++) {
			System.out.append('#');
		}
		for (int i = STATUS_BAR_SIZE; i > hashNum && i > 0; i--) {
			System.out.append(' ');
		}
		System.out.append(']');
		System.out.printf(" - %.2f%% ", percent * 100);
	}

	private void printSpeed(long progress) {
		long delta = progress - previous;
		double speed = (double) delta / period * SECOND;
		double avgSpeed = (double) progress / period * SECOND;
		System.out.printf("speed: %.2f MB/s\t average speed: %.2f MB/s", speed / MB, avgSpeed / MB);
	}

	private void tellSpeed(long progress) {
		double percent = total == 0.0 ? 0.0 : (double) progress / total;
		System.out.print("\n");
		printProgress(percent);
		printSpeed(progress);
	}

	@Override
	public void run() {
		try {
			while (!Thread.interrupted()) {
				long progress = sizeObserver.getCurrentSize();
//				System.out.println("progress: " + progress);
				total = sizeObserver.getTotalSize();
//				System.out.println("total: " + total);
				tellSpeed(progress);
				Thread.sleep(period);
			}
		} catch (InterruptedException e) {
			tellSpeed(total);
//			System.out.println("stopped");
		}
	}

	@Override
	public void stopped() {
		interrupt();
	}
}
