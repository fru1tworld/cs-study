import { ref } from 'vue'
import ChildComp from './ChildComp.vue'

export default {
  components: {
    ChildComp
  },
  setup() {
    const msg = ref('Vue는 개발자에게 정말 유용하죠! 🎁')

    return {
      msg
    }
  }
}
