import Dashboard from "./components/Dashboard.vue"
import Login from "./components/Login.vue"
import SystemManagement from './components/SystemManagement.vue'
import AddUser from './components/AddUser.vue'
import DelUser from './components/DelUser.vue'
import UpdateUser from './components/UpdateUser.vue'
import VehicleOps from './components/VehicleOps.vue'

import Order from './components/Order.vue'
import NewOrder from './components/NewOrder.vue'
import UnhandledOrder from './components/UnhandledOrder.vue'
import MyOrder from "./components/MyOrder.vue"
import QueryOrder from "./components/QueryOrder.vue"

import OilUser from './components/OilUser.vue'
import QueryOilUser from './components/QueryOilUser.vue'

import Intern from './components/Intern.vue'
import UpdateBuilder from './components/UpdateBuilder.vue'
import UpdateBuildCase from './components/UpdateBuildCase.vue'
import UsageReport from './components/UsageReport.vue'

import MyCase from './components/MyCase.vue'
import ObtainCase from './components/ObtainCase.vue'

export const routes = [{
        path: '/',
        component: Dashboard,
        name: 'Dashboard'
    },
    {
        path: '/Login',
        component: Login,
        name: 'Login'
    },
    {
        path: '/Vehicle',
        component: VehicleOps,
        name: 'VehicleOps',
        children: [{
                path: 'AddVehicle',
                component: AddUser,
                name: 'AddVehicle'
            },
            {
                path: 'DelVehicle',
                component: DelUser,
                name: 'DelVehicle'
            },
            {
                path: 'UpdateVehicle',
                component: UpdateUser,
                name: 'UpdateVehicle'
            },
        ]
    },
    {
        path: '/Order',
        component: Order,
        name: 'Order',
        children: [{
                path: 'Unhandled',
                component: UnhandledOrder,
                name: 'UnhandledOrder'
            },
            {
                path: 'New',
                component: NewOrder,
                name: 'NewOrder'
            },
            {
                path: 'Mine',
                component: MyOrder,
                name: 'MyOrder'
            },
            {
                path: 'Query',
                component: QueryOrder,
                name: 'QueryOrder'
            }
        ]
    },
    {
        path: '/OilUser',
        component: OilUser,
        name: 'OilUser',
        children: [{
            path: 'Query',
            component: QueryOilUser,
            name: 'QueryOilUser'
        }]
    },
    {
        path: '/Intern',
        component: Intern,
        name: 'Intern',
        children: [{
                path: 'Builder',
                component: UpdateBuilder,
                name: 'UpdateBuilder'
            },
            {
                path: 'BuildCase',
                component: UpdateBuildCase,
                name: 'UpdateBuildCase'
            },
            {
                path: 'Report',
                component: UsageReport,
                name: 'Report'
            },
        ]
    },
    {
        path: '/Sales',
        component: Intern,
        name: 'Sales',
        children: [{
                path: 'MyCase',
                component: MyCase,
                name: 'MyCase'
            },
            {
                path: 'ObtainCase',
                component: ObtainCase,
                name: 'ObtainCase'
            },
            {
                path: 'Map',
                component: UsageReport,
                name: 'Map'
            },
            {
                path: 'UsageReport',
                component: UsageReport,
                name: 'UsageReport'
            },
        ]
    },
    {
        path: '/PersonalReport',
        component: Intern,
        name: 'Report',
        children: [{
                path: 'UsageReport',
                component: UsageReport,
                name: 'UsageReport'
            }
        ]
    },
    {
        path: '/System',
        component: SystemManagement,
        name: 'SystemManagement',
        children: [{
                path: 'AddUser',
                component: AddUser,
                name: 'AddUser'
            },
            {
                path: 'DelUser',
                component: DelUser,
                name: 'DelUser'
            },
            {
                path: 'UpdateUser',
                component: UpdateUser,
                name: 'UpdateUser'
            },
        ]
    },
    {
        path: '*',
        redirect: '/'
    }
];