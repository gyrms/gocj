import java.io.*;
import java.util.*;

/**
 * 백준 11724 — 연결 요소의 개수
 *
 * TODO 빈칸을 채워 완성할 것.
 * 실행:  javac Main.java && java Main < input.txt    → 2
 *        javac Main.java && java Main < input2.txt   → 1
 */
public class Main {

    static List<Integer>[] graph;
    static boolean[] visited;

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        // ─────────────────────────────────────────
        // ① 첫 줄에 N M 이 "한 줄에 같이" 온다  ← 2606과 다름!
        //    힌트: StringTokenizer 로 쪼갤 것
        // ─────────────────────────────────────────
        int n = 0;   // TODO
        int m = 0;   // TODO


        // ② 그래프 준비 (2606과 동일)
        graph = new ArrayList[n + 1];
        // TODO: for문으로 graph[i] = new ArrayList<>();

        visited = new boolean[n + 1];


        // ③ 간선 m개 읽어서 양방향으로 (2606과 동일)
        for (int i = 0; i < m; i++) {
            // TODO

        }


        // ─────────────────────────────────────────
        // ④ 모든 정점을 시작점으로 시도  ← 이게 이 문제의 핵심!
        //    아직 방문 안 된 정점에서만 dfs 를 부르고, 부른 횟수를 센다
        // ─────────────────────────────────────────
        int count = 0;
        // TODO: for (int i = 1; i <= n; i++) {
        //           if (!visited[i]) { dfs(i); count++; }
        //       }

        System.out.println(count);
    }


    // 2606에서 쓴 것과 완전히 동일 — 그대로 옮겨오면 됨
    static void dfs(int now) {
        // TODO

    }
}
