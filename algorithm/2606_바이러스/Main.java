import java.io.*;
import java.util.*;

/**
 * 백준 2606 — 바이러스
 *
 * 빈칸(TODO)을 채워서 완성할 것.
 * 실행:  javac Main.java && java Main < input.txt
 * 정답:  4
 */
public class Main {

    static List<Integer>[] graph;   // graph[i] = i번과 연결된 컴퓨터 목록
    static boolean[] visited;       // 방문 체크
    static int count = 0;           // 감염된 컴퓨터 수

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        // ─────────────────────────────────────────
        // ① 입력 — 첫 줄: 컴퓨터 수 / 둘째 줄: 연결 개수
        //    힌트: Integer.parseInt(br.readLine().trim())
        // ─────────────────────────────────────────
        int n = 0;   // TODO
        int m = 0;   // TODO


        // ─────────────────────────────────────────
        // ② 그래프 준비
        //    번호가 1부터이므로 n+1 크기
        //    ⚠️ 배열만 만들면 안이 전부 null! 리스트를 하나씩 넣어야 함
        // ─────────────────────────────────────────
        graph = new ArrayList[n + 1];
        // TODO: for문 돌면서 graph[i] = new ArrayList<>();


        visited = new boolean[n + 1];


        // ─────────────────────────────────────────
        // ③ 연결 정보 m줄 읽기 — ⚠️ 양방향으로 넣을 것
        // ─────────────────────────────────────────
        for (int i = 0; i < m; i++) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            int a = Integer.parseInt(st.nextToken());
            int b = Integer.parseInt(st.nextToken());

            // TODO: a→b, b→a 두 줄

        }


        // ─────────────────────────────────────────
        // ④ 1번부터 탐색 시작
        // ─────────────────────────────────────────
        dfs(1);
        System.out.println(count);
    }


    static void dfs(int now) {
        visited[now] = true;              // ① 방문 처리

        // ② now의 이웃을 하나씩 순회
        // TODO: for (int next : graph[now]) {

               // ③ 아직 안 갔으면
               // TODO: if (!visited[next]) {

                      // count++ 하고 dfs(next) 호출
                      // TODO

        // }
    }
}
