export default {
  emits: ['response'],
  setup(props, { emit }) {
    emit('response', '자식 컴포넌트로부터 🌷를 받았어요!')
    return {}
  }
}
