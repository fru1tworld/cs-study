import { ref } from 'vue'

export default {
  setup() {
    // 각 할 일에 고유한 ID 부여
    let id = 0

    const newTodo = ref('')
    const todos = ref([
      { id: id++, text: 'HTML 배우기' },
      { id: id++, text: 'JavaScript 배우기' },
      { id: id++, text: 'Vue 배우기' }
    ])

    function addTodo() {
      // ...
      newTodo.value = ''
    }

    function removeTodo(todo) {
      // ...
    }

    return {
      newTodo,
      todos,
      addTodo,
      removeTodo
    }
  }
}
