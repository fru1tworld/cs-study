export default {
  emits: ['response'],
  created() {
    this.$emit('response', '자식 컴포넌트로부터 🌷를 받았어요!')
  }
}
