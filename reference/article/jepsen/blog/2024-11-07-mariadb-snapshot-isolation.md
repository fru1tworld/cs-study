# MariaDB 스냅샷 격리

날짜: 2024-10-24

2023년 말, Jepsen은 MySQL과 MariaDB의 `REPEATABLE READ`가 실제로 반복 가능한 읽기를 제공하지 않는다고 보고했습니다.

이에 대응하여 MariaDB 팀은 지난 한 해 동안 `--innodb-snapshot-isolation=true` 플래그를 추가했습니다. 이 새로운 플래그를 활성화하면 `REPEATABLE READ`가 "Lost Update, Non-repeatable Read, 그리고 Monotonic Atomic View 위반을 방지"하도록 작동합니다.

Jepsen은 아직 이 기능을 테스트하지 않았으나, 새 플래그가 활성화되었을 때 MariaDB가 `REPEATABLE READ` 수준에서 스냅샷 격리(Snapshot Isolation)를 제공할 수 있을 것으로 보인다고 평가했습니다.
