import { ref } from 'vue'

export default {
  setup() {
    const text = ref('수정하세요')
    const checked = ref(true)
    const checkedNames = ref(['잭'])
    const picked = ref('하나')
    const selected = ref('A')
    const multiSelected = ref(['A'])

    return {
      text,
      checked,
      checkedNames,
      picked,
      selected,
      multiSelected
    }
  }
}
