import { reactive, ref } from 'vue'

export default {
  setup() {
    const counter = reactive({ count: 0 })
    const message = ref('안녕 Vue!')

    return {
      counter,
      message
    }
  }
}
