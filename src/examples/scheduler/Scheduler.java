package scheduler;

import gov.nasa.jpf.abstraction.Verifier;

public class Scheduler
{
    public static void main(String[] args) {
        ThreadInfo[] id2thread = new ThreadInfo[3];

        // prepare a few threads
        id2thread[0] = new ThreadInfo(Verifier.unknownInt());
        id2thread[1] = new ThreadInfo(Verifier.unknownInt());
        id2thread[2] = new ThreadInfo(Verifier.unknownInt());


        // some threads are put into the active state
        id2thread[1].active = true;
        id2thread[2].active = true;

        // compute order in which threads should run
        int schedule_size = 0;
        int[] schedule = new int[SchedulerConfig.SIZE];

        // set of active threads is iterated when to make scheduling decisions
        for (int k = 0; k < id2thread.length; ++k) {
            ThreadInfo actTh = id2thread[k];

            if (!actTh.active) continue;

            if (schedule_size == 0)
            {
                schedule[0] = k;
                ++schedule_size;
            }
            else
            {
                // the info object is retrieved for each active thread
                for (int i = 0; i < schedule_size; ++i) {
                    ThreadInfo schTh = id2thread[schedule[i]];

                    if (actTh.priority > schTh.priority) {
                        // insert into the scheduling queue
                        for (int j = schedule_size - 1; j >= i; --j) {
                            schedule[j + 1] = schedule[j];
                        }
                           schedule[i] = k;
                        ++schedule_size;
                        break;
                    }
                }
            }
        }
    }

}

class SchedulerConfig {
    public static int SIZE = 5;
}

class ThreadInfo
{
    public int priority;
    public boolean active;

    public ThreadInfo(int prio_) {
        active = false;
        priority = prio_;
    }
}
