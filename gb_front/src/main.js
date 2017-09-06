import Vue from 'vue';
import VueRouter from 'vue-router';
import App from './App.vue';
import {routes} from  './route';
import {store} from './store/store';
import axios from 'axios'
import baseUrl from './baseUrl'
import moment from 'moment'

Vue.use(VueRouter);
const router = new VueRouter({
    routes
})


router.beforeEach((to, from, next)=>{
    if(to.name == 'Login' || store.getters.isAuthenticated)
        next(true)
    else
        next({name:'Login'})
})

//Setup axios config
axios.defaults.baseURL = baseUrl()
axios.defaults.withCredentials = true

//Setup moment
moment.locale('zh_tw')

import * as VueGoogleMaps from 'vue2-google-maps'
Vue.use(VueGoogleMaps, {
    load: {
        key: 'AIzaSyAi9hG7X74_CL-3i_6utBMNKzrRKOqwo98',
        libraries: 'places', // This is required if you use the Autocomplete plugin
        // OR: libraries: 'places,drawing'
        // OR: libraries: 'places,drawing,visualization'
        // (as you require)
    }
})

new Vue({
    el: '#app',
    store,
    router,
    render: h => h(App)
})
