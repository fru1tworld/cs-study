import TodoItem from './TodoItem.vue'

export default {
  components: {
    TodoItem
  },
  data() {
    return {
      groceryList: [
        { id: 0, text: '채소' },
        { id: 1, text: '치즈' },
        { id: 2, text: '사람들이 먹어야 하는 다른 모든 것' }
      ]
    }
  }
}
