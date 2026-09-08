// 각 할 일에 고유한 ID 부여
let id = 0

export default {
  data() {
    return {
      newTodo: '',
      todos: [
        { id: id++, text: 'HTML 배우기' },
        { id: id++, text: 'JavaScript 배우기' },
        { id: id++, text: 'Vue 배우기' }
      ]
    }
  },
  methods: {
    addTodo() {
      // ...
      this.newTodo = ''
    },
    removeTodo(todo) {
      // ...
    }
  }
}
