import { ref } from 'vue'
import ChildComp from './ChildComp.vue'

export default {
  components: {
    ChildComp
  },
  setup() {
    const childMsg = ref('자식 컴포넌트로부터 아직 메시지를 받지 못했어요!')

    return {
      childMsg
    }
  }
}
