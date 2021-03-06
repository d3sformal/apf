package svcomp.loops;

public class Eureka01TrueUnreachableLabel {
    private static int INFINITY = 899;

    public static void main(String[] args) {
        int nodecount = 5;
        int edgecount = 4;//20;
        int source = 0;
        /*
        int[] Source = new int[] {0, 4, 1, 1, 0, 0, 1, 3, 4, 4, 2, 2, 3, 0, 0, 3, 1, 2, 2, 3};
        int[] Dest = new int[] {1, 3, 4, 1, 1, 4, 3, 4, 3, 0, 0, 0, 0, 2, 3, 0, 2, 1, 0, 4};
        int[] Weight = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
        int[] distance = new int[5];
        */
        int[] Source = new int[4];
        int[] Dest = new int[4];
        int[] Weight = new int[4];
        int[] distance = new int[5];

        Source[0] = 0;
        Source[1] = 4;
        Source[2] = 1;
        Source[3] = 1;

        Dest[0] = 1;
        Dest[1] = 3;
        Dest[2] = 4;
        Dest[3] = 1;

        Weight[0] = 0;
        Weight[1] = 1;
        Weight[2] = 2;
        Weight[3] = 3;

        for (int i = 0; i < nodecount; i++) {
            if (i == source) {
                distance[i] = 0;
            } else {
                distance[i] = INFINITY;
            }
        }

        for (int i = 0; i < nodecount; i++) {
            for (int j = 0; j < edgecount; j++) {
                int x = Dest[j];
                int y = Source[j];

                if (distance[x] > distance[y] + Weight[j]) {
                    distance[x] = distance[y] + Weight[j];
                }
            }
        }

        for (int i = 0; i < edgecount; i++) {
            int x = Dest[i];
            int y = Source[i];

            if (distance[x] > distance[y] + Weight[i]) {
                return;
            }
        }

        for (int i = 0; i < nodecount; i++) {
            assert distance[i] >= 0;
        }
    }
}

