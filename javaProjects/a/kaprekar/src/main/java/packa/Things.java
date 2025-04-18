public class Things {
    private static boolean setWay(int[][] map, int i, int j) {
        if (map[6][5] == 2) {
            return true;
        }

        // If the current position is unvisited (0), explore it
        if (map[i][j] == 0) {
            // Mark the current position as '2'
            map[i][j] = 2;

            // Move down
            if (setWay(map, i + 1, j)) {
                return true;
            }
            // Move right
            else if (setWay(map, i, j + 1)) {
                return true;
            }
            // Move up
            else if (setWay(map, i - 1, j)) {
                return true;
            }
            // Move left
            else if (setWay(map, i, j - 1)) {
                return true;
            }

            map[i][j] = 3; // Mark as dead end (3) if no direction worked
            return false;
        }
        return false;
    }
}
