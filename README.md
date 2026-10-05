# StatSystem (Paper 1.21.10 + MagicSpells)

## 빌드 방법
A) 내 컴퓨터: JDK 21 + Gradle 설치 후 이 폴더에서 `gradle build` -> `build/libs/StatSystem-1.0.0.jar`
B) GitHub: 이 폴더를 새 저장소에 올리면 Actions 가 자동 빌드 -> Actions 탭의 Artifacts 에서 jar 다운로드

## 설치
jar 를 서버 `plugins/` 에 넣고 재시작. 설정: `plugins/StatSystem/config.yml`

## 명령어
/스탯                      스탯 창 (좌클릭 +1, Shift+좌클릭 +10)
/스탯 정보 [플레이어]
/스탯 올리기 <힘|민첩|체력|마법> [수]
/스탯 초기화 <플레이어|전체>   (관리자)
/스탯 리로드                (관리자, config 적용)
/레벨설정 <플레이어> <레벨>
/경험치지급 <플레이어> <양>
/포인트지급 <플레이어> <수>
/스탯무기 설정 <스킬퍼뎀|공격력|크확|크뎀|생명력흡수> <값> / 제거 / 확인
