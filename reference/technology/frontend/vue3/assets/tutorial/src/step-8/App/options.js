let id = 0

export default {
  data() {
    return {
      newTodo: '',
      hideCompleted: false,
      todos: [
        { id: id++, text: 'HTML 배우기', done: true },
        { id: id++, text: 'JavaScript 배우기', done: true },
        { id: id++, text: 'Vue 배우기', done: false }
      ]
    }
  },
  computed: {
    // ...
  },
  methods: {
    addTodo() {
      this.todos.push({ id: id++, text: this.newTodo, done: false })
      this.newTodo = ''
    },
    removeTodo(todo) {
      this.todos = this.todos.filter((t) => t !== todo)
    }
  }
}
