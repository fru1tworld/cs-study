import ChildComp from './ChildComp.vue'

export default {
  components: {
    ChildComp
  },
  data() {
    return {
      greeting: '부모 컴포넌트로부터 💌을 전달받았어요!'
    }
  }
}
