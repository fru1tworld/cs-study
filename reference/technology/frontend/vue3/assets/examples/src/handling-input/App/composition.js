import { ref } from 'vue'

export default {
  setup() {
    const message = ref('Hello World!')

    function reverseMessage() {
      // ref의 값을 접근/변경하려면
      // .value 속성을 사용하세요.
      message.value = message.value.split('').reverse().join('')
    }

    function notify() {
      alert('이동이 차단되었습니다.')
    }

    return {
      message,
      reverseMessage,
      notify
    }
  }
}
