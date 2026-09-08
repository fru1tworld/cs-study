import { ref } from 'vue'

export default {
  setup() {
    // "ref"는 값을 저장하는 반응형 데이터 소스입니다.
    // 기술적으로는 문자열을 ref()로 감싸지 않아도
    // 표시할 수 있지만, 다음 예제에서 값을 변경하려고 할 때
    // 왜 ref()가 필요한지 알게 될 것입니다.
    const message = ref('Hello World!')

    return {
      message
    }
  }
}
