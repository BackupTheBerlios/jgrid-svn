package benchmark;
/*
 * Copyright (c) 1996, 1997 by Doug Bell <dbell@shvn.com>.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

class PingPongThread extends Thread {
	static int			count;
	static boolean		go;
	static int			iterations;
	Object				ball;
	Benchmark			runner;
	int					sample;
	PingPongThread		partner;

	PingPongThread (Benchmark runner, int sample) {
		this.runner = runner;
		this.sample = sample;
		ball = new Object();
		iterations = 0;
		go = true;
		count = 2;
		partner = new PingPongThread();
		partner.ball = ball = new Object();
		partner.start();
		start();
	}

	PingPongThread () {}

	public void run () {
		if (runner != null)
			runner.startTimer(false);
		try {
			synchronized (ball) {
				Thread.yield();  // without this, Netscape will deadlock
				ball.notify();
				try {
					while (go) {
						iterations++;
						ball.wait();
						ball.notify();
					}
				}
				catch (InterruptedException e) {}
			}
		}
		finally {
			if (runner != null)
				runner.stopTimer(sample, iterations);
			count--;
		}
	}
}  // class PingPongThread
