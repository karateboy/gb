import Dashboard from "./components/Dashboard.vue"
import Login from "./components/Login.vue"
import SystemManagement from './components/SystemManagement.vue'
import AddUser from './components/AddUser.vue'
import DelUser from './components/DelUser.vue'
import UpdateUser from './components/UpdateUser.vue'
import VehicleOps from './components/VehicleOps.vue'

import Order from  './components/Order.vue'
import NewOrder from './components/NewOrder.vue'
import MyOrder from "./components/MyOrder.vue"
import QueryOrder from "./components/QueryOrder.vue"

export const routes = [
    {path: '/', component: Dashboard, name: 'Dashboard'},
    {path: '/Login', component: Login, name: 'Login'},
    {
        path: '/Vehicle', component: VehicleOps, name: 'VehicleOps',
        children: [
            {path: 'AddVehicle', component:AddUser, name:'AddVehicle' },
            {path: 'DelVehicle', component:DelUser, name:'DelVehicle' },
            {path: 'UpdateVehicle', component:UpdateUser, name:'UpdateVehicle' },
        ]
    },
    {
        path: '/Order', component: Order, name: 'Order',
        children: [
            {path: 'New', component: NewOrder, name: 'NewOrder'},
            {path: 'Mine', component: MyOrder, name: 'MyOrder'},
            {path: 'Query', component: QueryOrder, name: 'QueryOrder'}
        ]
    },
    {
        path: '/CareHouse', component: CareHouse, name: 'CareHouse',
        children: [
            {path: 'New', component: NewOrder, name: 'NewCareHouse'},
            {path: 'Query', component: QueryOrder, name: 'QueryCareHouser'}
        ]
    },
    {
        path: '/System', component: SystemManagement, name: 'SystemManagement',
        children: [
            {path: 'AddUser', component:AddUser, name:'AddUser' },
            {path: 'DelUser', component:DelUser, name:'DelUser' },
            {path: 'UpdateUser', component:UpdateUser, name:'UpdateUser' },
        ]
    },
    {path: '*', redirect: '/'}
];
