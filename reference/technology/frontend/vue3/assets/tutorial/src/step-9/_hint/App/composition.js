import { ref, onMounted } from 'vue'

export default {
  setup() {
    const pElementRef = ref(null)

    onMounted(() => {
      pElementRef.value.textContent = '마운트 되었어요!'
    })

    return {
      pElementRef
    }
  }
}
