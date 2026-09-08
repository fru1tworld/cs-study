import DemoGrid from './Grid.vue'
import { ref } from 'vue'

export default {
  components: {
    DemoGrid
  },
  setup() {
    const searchQuery = ref('')
    const gridColumns = ['name', 'power']
    const gridData = [
      { name: '척 노리스', power: Infinity },
      { name: '브루스 리', power: 9000 },
      { name: '성룡', power: 7000 },
      { name: '이연걸', power: 8000 }
    ]

    return {
      searchQuery,
      gridColumns,
      gridData
    }
  }
}
