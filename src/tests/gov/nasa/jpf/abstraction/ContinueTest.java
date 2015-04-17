package gov.nasa.jpf.abstraction;

// Taken from CPAchecker repository
public class ContinueTest extends BaseTest {
    public ContinueTest() {
        config.add("+panda.refinement=true");
    }

    @Test
    public static void test() {
        int y = 0;
        int x = 0;
        int z = 0;

    label:
        while (y < 2) {
            y++;
            x = 0;

            while(x < 2) {
                x++;

                if(x > 1) {
                  continue label;
                }

                z++;
            }
        }

        System.out.println(y);

        assert z == 2; // 2 loops above, so z == 2 always true
    }
}
