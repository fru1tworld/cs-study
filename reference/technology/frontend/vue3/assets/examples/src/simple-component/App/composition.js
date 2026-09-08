import { ref } from 'vue'
import TodoItem from './TodoItem.vue'

export default {
  components: {
    TodoItem
  },
  setup() {
    const groceryList = ref([
      { id: 0, text: '채소' },
      { id: 1, text: '치즈' },
      { id: 2, text: '사람들이 먹어야 하는 그 밖의 것들' }
    ])

    return {
      groceryList
    }
  }
}
