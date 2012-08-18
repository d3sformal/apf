package gov.nasa.jpf.abstraction.BoundedBuffer;

/* from http://www.doc.ic.ac.uk/~jnm/book/ */
/* Concurrency: State Models & Java Programs - Jeff Magee & Jeff Kramer */
/* has a deadlock */

/******************* PRODUCER ************************/

class Producer extends Thread {

	Buffer buf;

	Producer(Buffer b) {
		buf = b;
	}

	public void run() {
		try {
			// int tmp = 0;
			while (true) {
				// buf.put(new Integer(tmp));
				// System.out.println(this + " produced " + tmp);
				// tmp=tmp+1;
				buf.put();
			}
		} catch (InterruptedException e) {
		}
	}
}
