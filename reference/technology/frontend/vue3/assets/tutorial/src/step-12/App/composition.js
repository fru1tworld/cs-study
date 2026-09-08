import { ref } from 'vue'
import ChildComp from './ChildComp.vue'

export default {
  components: {
    ChildComp
  },
  setup() {
    const greeting = ref('부모 컴포넌트로부터 💌을 전달받았어요!')

    return {
      greeting
    }
  }
}
